import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    errors: ['rate<0.1'],
  },
};

const queries = [
  'machine learning',
  'artificial intelligence',
  'neural networks',
  'deep learning',
  'natural language processing',
];

export default function () {
  const query = queries[Math.floor(Math.random() * queries.length)];
  
  const payload = JSON.stringify({
    q: query,
    k: 10,
    alpha: 0.5,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post('http://localhost:8080/api/v1/search', payload, params);

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response has results': (r) => JSON.parse(r.body).results !== undefined,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  errorRate.add(!success);

  sleep(0.5);
}
