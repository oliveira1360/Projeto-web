import {BASE_URL, getToken} from "../../utils/comman";
import {request} from "../request";


export type PlayerInfoResponse = {
    userId: number;
    name: string;
    nickName: string;
    email: string;
    balance: string;
    imageUrl?: string;
    _links: any;
};


export const playerService = {
    async playerInfo(): Promise<PlayerInfoResponse> {
        const response = await request(`/user/info`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        });

        const data = await response.json();

        return {
            userId: data.userId,
            name: data.name,
            nickName: data.nickName,
            email: data.email,
            balance: data.balance,
            imageUrl: data.imageUrl,
            _links: data._links,
        };
    }
};

