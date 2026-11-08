function isProduction(): boolean {
  return process.env.NODE_ENV === "production";
}

export function serverEndpoint(): string {
  // Set at build time (Next inlines it), for example by the browser tests, which build a bundle
  // that talks to a local backend instead of production. Written out in full because Next only
  // replaces the literal `process.env.NEXT_PUBLIC_API_URL`.
  const override = process.env.NEXT_PUBLIC_API_URL;
  if (override) {
    return override;
  }
  if (isProduction()) {
    return "https://geogames-web-service.onrender.com";
  } else {
    return "http://localhost:8080";
  }
}
