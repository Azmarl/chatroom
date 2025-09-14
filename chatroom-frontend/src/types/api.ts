// Describes the user info we get after a successful login
// and what we store in localStorage
export interface UserInfo {
  id: number;
  username: string;
  nickname: string;
  avatarUrl: string;
}

export interface GroupUserInfo {
  id: number;
  username: string;
  nickname: string;
  avatarUrl: string;
  role: 'owner' | 'admin' | 'member';
}

/**
 * 对应后端 LoginResponse DTO 的完整登录响应接口。
 * 这就是您需要修复或创建的类型。
 */
export interface LoginResponse {
  accessToken: string; // <-- 确保这个字段存在
  userInfo: UserInfo;
}

// 对应后端的 PendingRequestDto
export interface PendingRequest {
  requestType: 'FRIEND_REQUEST' | 'GROUP_JOIN_REQUEST';
  timestamp: string;
  requesterId: number;
  requesterNickname: string;
  requesterAvatarUrl: string;
  friendshipId?: number; // 好友请求ID
  conversationId?: number; // 群聊ID
  conversationName?: string; // 群聊名称

  // (新增) 前端UI状态，用于处理按钮的交互
  actionStatus?: 'idle' | 'processing' | 'accepted' | 'rejected' | 'error';
}

export interface ConversationSummary {
  conversationId: number;
  uuid: string;
  type: 'PRIVATE' | 'GROUP'; // 假设后端返回的是字符串枚举
  name: string;
  avatarUrl: string;
  lastMessageContent: string;
  lastMessageTimestamp: string | null; // 先作为字符串接收
  unreadCount: number;
  isPinned: boolean;
  areNotificationsMuted: boolean;
}

export interface GroupDetailsDto {
  conversationId: number;
  uuid: string;
  name: string;
  avatarUrl: string;
  description: string;
  memberCount: number;
  members: GroupUserInfo[]; // 成员列表
  currentUserRole: 'owner' | 'admin' | 'member';
}

export interface Sender {
  id: number;
  nickname: string;
  avatarUrl: string;
}

export interface RepliedMessageInfo {
  messageId: number;
  senderNickname: string;
  content: string;
}

/**
 * 与后端 MessageDto 完全匹配的消息数据接口
 */
export interface MessageDto {
  id: number;
  conversationId: number;
  content: string;
  timestamp: string; // 后端 LocalDateTime 会被序列化为 ISO 字符串
  sender: Sender;
  messageType: 'text' | 'image' | 'file' | 'emoji' | 'system'; 
  repliedMessage?: RepliedMessageInfo; // 可选字段，现在是一个对象
  recalled?: boolean; // (核心修改) 字段名与后端保持一致
}

export type MessageType = 'text' | 'image' | 'file' | 'emoji' | 'system';

export interface GroupJoinRequestDto {
  requesterId: number;
  nickname: string;
  avatarUrl: string;
}

export interface AdminActionRequest {
  targetUserId: number;
  action: 'PROMOTE' | 'DEMOTE';
}

export interface HandleJoinRequest {
  requesterId: number;
  action: 'ACCEPT' | 'REJECT';
}

export interface ConversationBlock {
  id: number;
  conversationId: number;
  blockedUser: UserInfo;
  blockerUser: UserInfo;
  reason: string;
  createdAt: string; // ISO 日期字符串
}

export interface MutedUserDto {
    userId: number;
    nickname: string;
    avatarUrl: string;
    mutedUntil: string; // The ISO timestamp string when the mute expires.
}

export interface GroupInvitation {
  type: string;
  groupId: number;
  groupUuid: string;
  groupName: string;
  inviterId: number;
  inviterName: string;
  timestamp: number;
}