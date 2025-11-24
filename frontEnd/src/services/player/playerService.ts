import {request} from "../request";
import {PlayerInfoResponse} from "./playerResponseTypes";




export const playerService = {
    /**
     * GET /user/info
     * Obtém informações do jogador autenticado.
     */
    async playerInfo(): Promise<PlayerInfoResponse> {
        const response = await request(`/user/info`, {
            method: 'GET',
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
