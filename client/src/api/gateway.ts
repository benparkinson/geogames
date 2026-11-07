function isProduction(): boolean {
  return process.env.NODE_ENV === "production";
}

export function serverEndpoint(): string {
  if (isProduction()) {
    return "https://geogames-web-service.onrender.com";
  } else {
    return "http://localhost:8080";
  }
}
