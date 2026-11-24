// Public build-time settings. Configure the web server to proxy /api to Spring Boot.
// Never put JWT signing secrets, database passwords or account credentials here.
export const environment = { production: true, apiUrl: '/api' };
