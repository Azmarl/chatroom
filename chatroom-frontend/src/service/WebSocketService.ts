import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { GroupInvitation } from '@/types/api';
import { useAuthStore } from '@/stores/auth';

// 定义消息处理器回调函数的类型
export type MessageCallback = (message: any) => void;

class WebSocketService {
  private stompClient: Client;
  private subscriptions: Map<string, any> = new Map();
  private pendingSubscriptions: Map<string, MessageCallback[]> = new Map();
  
  // 分开存储两种不同类型的回调
  private friendRequestCallbacks: MessageCallback[] = [];
  private groupInvitationCallbacks: ((invitation: GroupInvitation) => void)[] = [];
  
  // 添加连接状态跟踪
  private isConnected: boolean = false;
  private isConnecting: boolean = false;
  private connectionCallbacks: (() => void)[] = [];

  private getUserId(): number | null {
    try {
      const authStore = useAuthStore();
      return authStore.userInfo?.id || null;
    } catch (error) {
      console.error('Failed to get user ID:', error);
      return null;
    }
  }

  constructor() {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      debug: (str) => {
        console.log('STOMP: ' + str);
      },
      reconnectDelay: 5000,
    });

    this.setupStompHandlers();
  }

  // 设置 STOMP 事件处理器
  private setupStompHandlers(): void {
    this.stompClient.onConnect = (frame) => {
      console.log('Connected to WebSocket:', frame);
      this.isConnected = true;
      this.isConnecting = false;
      
      this.connectionCallbacks.forEach(callback => callback());
      this.connectionCallbacks = [];
      
      // 连接成功后，处理所有待处理和之前的订阅
      this.resubscribeAll();
      this.processPendingSubscriptions();
    };

    this.stompClient.onDisconnect = () => {
      console.log('Disconnected from WebSocket');
      this.isConnected = false;
      this.isConnecting = false;
    };

    this.stompClient.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
      this.isConnecting = false;
    };

    this.stompClient.onWebSocketError = (event) => {
      console.error('WebSocket error:', event);
      this.isConnecting = false;
    };

    this.stompClient.onWebSocketClose = (event) => {
      console.log('WebSocket closed:', event);
      this.isConnected = false;
      this.isConnecting = false;
    };
  }

  public connect(onConnectCallback?: () => void): void {
    // 如果已连接或正在连接，则只添加回调，不执行任何操作
    if (this.isConnected || this.isConnecting) {
      if (this.isConnected && onConnectCallback) {
        onConnectCallback(); // 如果已连接，立即执行回调
      } else if (onConnectCallback) {
        this.connectionCallbacks.push(onConnectCallback);
      }
      return;
    }

    const token = localStorage.getItem('accessToken');
    if (!token) {
      console.error("No access token found for WebSocket connection.");
      return;
    }

    this.stompClient.connectHeaders = {
      'Authorization': `Bearer ${token}`
    };

    if (onConnectCallback) {
      this.connectionCallbacks.push(onConnectCallback);
    }

    try {
      this.isConnecting = true;
      this.stompClient.activate();
    } catch (error) {
      console.error('Failed to activate STOMP client:', error);
      this.isConnecting = false;
    }
  }

  public disconnect(force: boolean = false): void {
    if (force || this.stompClient.active) {
      this.stompClient.deactivate();
      console.log('Disconnected from WebSocket.');
    }
    this.subscriptions.clear();
    this.isConnected = false;
  }

  // 重新订阅所有之前的订阅
  private resubscribeAll(): void {
    // 重新订阅用户通知（包括好友请求和群聊邀请）
    if (this.friendRequestCallbacks.length > 0 || this.groupInvitationCallbacks.length > 0) {
      this.subscribeToUserNotifications();
    }
  }

  // 安全的订阅方法，确保连接建立后再订阅
  private safeSubscribe(topic: string, callback: (message: IMessage) => void): void {
    if (!this.isConnected) {
      console.warn(`STOMP client not connected. Queuing subscription for ${topic}`);
      
      // 1. 将订阅请求加入待处理列表
      if (!this.pendingSubscriptions.has(topic)) {
        this.pendingSubscriptions.set(topic, []);
      }
      this.pendingSubscriptions.get(topic)?.push(callback);
      
      // 2. 确保连接正在进行 (如果未连接，connect会自动处理)
      this.connect();
      return;
    }

    if (this.subscriptions.has(topic)) {
      console.warn(`Already subscribed to ${topic}. Skipping.`);
      return;
    }

    try {
      const subscription = this.stompClient.subscribe(topic, callback);
      this.subscriptions.set(topic, subscription);
      console.log(`Subscribed to ${topic}`);
    } catch (error) {
      console.error(`Failed to subscribe to ${topic}:`, error);
    }
  }

  // 处理待处理的订阅请求
  private processPendingSubscriptions(): void {
    this.pendingSubscriptions.forEach((callbacks, topic) => {
      callbacks.forEach(callback => {
        this.safeSubscribe(topic, callback);
      });
    });
    this.pendingSubscriptions.clear();
  }

  // 其他订阅方法
  public subscribe(conversationId: number, callback: MessageCallback): void {
    const topic = `/topic/conversations/${conversationId}`;
    
    this.safeSubscribe(topic, (message: IMessage) => {
      try {
        const parsedMessage = JSON.parse(message.body);
        callback(parsedMessage);
      } catch (error) {
        console.error('Could not parse JSON:', message.body);
      }
    });
  }
  
  public subscribeToRecalls(conversationId: number, callback: MessageCallback): void {
    const topic = `/topic/conversations/${conversationId}/recalls`;
    
    this.safeSubscribe(topic, (message: IMessage) => {
      try {
        const parsedNotification = JSON.parse(message.body);
        callback(parsedNotification);
      } catch (error) {
        console.error('Could not parse recall notification JSON:', message.body);
      }
    });
  }
  
  public unsubscribe(conversationId: number): void {
    const topics = [
      `/topic/conversations/${conversationId}`,
      `/topic/conversations/${conversationId}/recalls`
    ];

    topics.forEach(topic => {
      const subscription = this.subscriptions.get(topic);
      if (subscription) {
        subscription.unsubscribe();
        this.subscriptions.delete(topic);
        console.log(`Unsubscribed from ${topic}`);
      }
    });
  }

  // 添加调试方法
  public debugSubscriptions(): void {
    console.log('Current subscriptions:', Array.from(this.subscriptions.keys()));
    console.log('Friend request callbacks:', this.friendRequestCallbacks.length);
    console.log('Group invitation callbacks:', this.groupInvitationCallbacks.length);
    console.log('Connection status:', this.isConnected ? 'Connected' : 'Disconnected');
  }

  public subscribeToUserNotifications(): void {
    const userId = this.getUserId();
    if (userId === null) {
      console.error('Cannot subscribe to user notifications: user ID is null.');
      return;
    }
    
    const topic = `/user/${userId}/queue/notifications`;
    
    // 如果已经订阅了这个主题，先取消订阅
    if (this.subscriptions.has(topic)) {
      this.subscriptions.get(topic)?.unsubscribe();
      this.subscriptions.delete(topic);
    }
    
    this.safeSubscribe(topic, (message: IMessage) => {
      try {
        const notification = JSON.parse(message.body);
        console.log('Received user notification:', notification);
        
        // 根据通知类型分发给不同的回调数组
        switch (notification.type) {
          case 'FRIEND_REQUEST':
            this.friendRequestCallbacks.forEach(cb => cb(notification));
            break;
          case 'GROUP_INVITATION':
            this.groupInvitationCallbacks.forEach(cb => cb(notification));
            break;
          default:
            console.warn('Received unknown notification type:', notification.type);
        }
      } catch (error) {
        console.error('Could not parse user notification JSON:', message.body);
      }
    });
  }

  // 注册回调的方法，但不触发订阅
  public onFriendRequest(callback: MessageCallback): void {
    this.friendRequestCallbacks.push(callback);
  }

  public onGroupInvitation(callback: (invitation: GroupInvitation) => void): void {
    this.groupInvitationCallbacks.push(callback);
  }
  
  // 移除回调的方法
  public removeFriendRequestCallback(callback: MessageCallback): void {
    const index = this.friendRequestCallbacks.indexOf(callback);
    if (index > -1) {
      this.friendRequestCallbacks.splice(index, 1);
    }
  }
  
  public removeGroupInvitationCallback(callback: (invitation: GroupInvitation) => void): void {
    const index = this.groupInvitationCallbacks.indexOf(callback);
    if (index > -1) {
      this.groupInvitationCallbacks.splice(index, 1);
    }
  }

  public isActive(): boolean {
    return this.stompClient.active;
  }
}

// 导出一个单例，确保整个应用只使用一个WebSocket连接
export const webSocketService = new WebSocketService();