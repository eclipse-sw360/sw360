export default interface RequestContent {
    method: string,
    headers: {
        'Content-Type'?: string,
        'Authorization'?: string
    },
    body: string | null
}