

export type PlayerInfoResponse = {
    userId: number;
    name: string;
    nickName: string;
    email: string;
    balance: string;
    imageUrl?: string;
    _links: any;
};

export type PlayerStatsResponse = {
    userId: number;
    totalGamesPlayed: number;
    totalWins: number;
    totalLosses: number;
    totalPoints: number;
    longestStreak: number;
    currentStreak: number;
    _links: any;
}