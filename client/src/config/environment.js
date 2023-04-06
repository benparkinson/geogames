export default function isProduction() {
  return process.env.RUNNING_ENV === "production";
}
