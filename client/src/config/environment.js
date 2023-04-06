export default function isProduction() {
  console.log(process.env.NODE_ENV);
  return process.env.NODE_ENV === "production";
}
