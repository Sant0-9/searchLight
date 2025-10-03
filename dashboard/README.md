# Searchlight Dashboard

Modern Next.js 14 dashboard for the Searchlight API.

## Features

-  Real-time hybrid search interface
- 🎚 Adjustable alpha parameter (keyword ↔ vector balance)
-  Result scoring visualization
-  Clean, responsive UI with Tailwind CSS

## Development

```bash
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000)

## Environment Variables

Create `.env.local`:

```
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
```

## Docker

```bash
docker build -t searchlight-dashboard .
docker run -p 3000:3000 searchlight-dashboard
```
