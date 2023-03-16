export function GuessBox({ countryName, index }) {
  console.log("Pss...guess " + countryName);
  const placeholder = "Country " + index;
  return (
    <div className="m-1">
      <input
        className="answer-attempt form-control"
        type="text"
        autoComplete="chrome-off"
        placeholder={placeholder}
      ></input>
    </div>
  );
}
