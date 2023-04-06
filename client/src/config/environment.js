export default function isProduction() {
  console.log(process.env.REACT_APP_RUNNING_ENV);
  return process.env.REACT_APP_RUNNING_ENV === "production";
}
