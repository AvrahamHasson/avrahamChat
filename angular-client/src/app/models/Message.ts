export interface Message {
  text: string;
  timestamp: string;
  sender: string;
  target?: string;
  targetUsername?: string;
  status: 'sent' | 'received' | 'read';
}