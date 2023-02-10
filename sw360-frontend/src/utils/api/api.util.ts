import { SW360_API_URL } from '../env';
import RequestContent from '../../object-types/RequestContent';

const base = SW360_API_URL + '/resource/api';

async function send({ method, path, data, token } : { method: string, path: string, data: Object | null, token: string }): Promise<any>{
	const request_content: RequestContent = { method, headers: {}, body: null };

	if (data) {
		request_content.headers['Content-Type'] = 'application/json';
		request_content['body']= JSON.stringify(data);
	}

	if (token) {
		request_content.headers['Authorization'] = `Bearer ${token}`;
	}

	return fetch(`${base}/${path}`, request_content)
		.then((r) => r.text())
		.then((json) => {
			try {
				return JSON.parse(json);
			} catch (err) {
				return json;
			}
		});
}

function GET(path: string, token: string) {
	return send({ method: 'GET', path, token, data: null });
}

function DELETE(path: string, token: string) {
	return send({ method: 'DELETE', path, token, data: null });
}

function POST(path: string, data: Object, token: string) {
	return send({ method: 'POST', path, data, token });
}

function PUT(path: string, data: Object, token: string) {
	return send({ method: 'PUT', path, data, token });
}

const ApiUtils = { GET, DELETE, POST, PUT };

export default ApiUtils