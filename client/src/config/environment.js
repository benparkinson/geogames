export default function isProduction() {
  console.log(process.env.RUNNING_ENV);
  return process.env.RUNNING_ENV === "production";
}
