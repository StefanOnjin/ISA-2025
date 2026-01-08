import { Comment } from './comment';

export interface CommentPage {
  content: Comment[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
