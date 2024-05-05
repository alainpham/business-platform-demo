import http from 'k6/http';
import { check, sleep } from 'k6';

// Define the base URL of your API
const BASE_URL = __ENV.BASE_URL || 'http://demovm.duckdns.org:8080';

// Define the options for your test
export let options = {
    stages: [
        { duration: '30s', target: 20 }, // Ramp up to 50 virtual users over 1 minute
        { duration: '4m', target: 50 }, // Stay at 50 virtual users for 3 minutes
        { duration: '30s', target: 0 }   // Ramp down to 0 virtual users over 1 minute
    ],
};

// Define the main function that represents your test scenario
export default function () {
    let body;
    let headers;
    let response;

    body = {
        "id": 0,
        "birthDate": "2024-03-13T13:49:14.808Z",
        "name": "string",
        "email": "string"
      };

    headers = {
        'Content-Type': 'application/json',
    };
    response = http.post(`${BASE_URL}/persons`, JSON.stringify(body), { headers: headers });

    check(response, {
        'Status 200': (r) => r.status === 200,
    });

    sleep(1);


    body = {
        "id": 0,
        "customerName": "alain",
        "bookingStartDate": "2023-12-10",
        "bookingEndDate": "2023-12-15",
        "hotel": "accor",
        "room": "5"
      };

    headers = {
        'Content-Type': 'application/json',
    };
    response = http.post(`${BASE_URL}/book`, JSON.stringify(body), { headers: headers });

    check(response, {
        'Status 200': (r) => r.status === 200,
    });

    sleep(1);


    body = {
        "id": 0,
        "customerName": "alain",
        "bookingStartDate": "2023-12-01",
        "bookingEndDate": "2023-12-01",
        "hotel": "accor",
        "room": "5"
      };

    headers = {
        'Content-Type': 'application/json',
    };
    response = http.post(`${BASE_URL}/book`, JSON.stringify(body), { headers: headers });

    check(response, {
        'Status 200': (r) => r.status === 200,
    });

    sleep(1);


}