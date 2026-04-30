import { Message } from "./Message";
import { User } from "./User";

export interface Chat {
  id: string;
  participants: User[];
  messages: Message[];
  lastMessageTime: string;
}