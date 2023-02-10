export default interface OAuthClient {
    description: string,
    client_id: string,
    client_secret: string,
    authorities: Array<string>
    scope: Array<string>
    access_token_validity: number,
    refresh_token_validity: number
}