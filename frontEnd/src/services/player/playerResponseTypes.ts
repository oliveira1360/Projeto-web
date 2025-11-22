

export type PlayerInfoResponse = {
    userId: number;
    name: string;
    nickName: string;
    email: string;
    balance: string;
    imageUrl?: string; // Opcional
    _links: any;
};