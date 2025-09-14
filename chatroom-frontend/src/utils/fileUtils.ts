/**
 * 检查文件名是否为常见的图片格式。
 * @param fileName 文件名
 * @returns 如果是图片则返回 true，否则返回 false
 */
export function isImageFile(fileName: string): boolean {
  if (!fileName) {
    return false;
  }
  const lowerCaseName = fileName.toLowerCase();
  return /\.(jpg|jpeg|png|gif|bmp|webp)$/.test(lowerCaseName);
}
export function parseFileContent(content: string) {
  try {
    const fileData = JSON.parse(content);
    return {
      url: fileData.url || '#',
      name: fileData.name || '未知文件',
      size: fileData.size || 0,
    };
  } catch {
    // 兼容旧的纯文本URL消息
    return { url: content, name: '文件', size: 0 };
  }
};
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};