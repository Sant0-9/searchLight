import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'Searchlight Dashboard',
  description: 'Hybrid search interface for Searchlight',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className="bg-gray-50">{children}</body>
    </html>
  )
}
