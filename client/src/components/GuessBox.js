export function GuessBox({ value, name, onChange, correct, disabled }) {
  function readCorrectness() {
    if (correct) {
      return " correct";
    } else if (false === correct) {
      return " incorrect";
    } else return "";
  }

  return (
    <div className="m-1">
      <input
        className={"answer-attempt form-control" + readCorrectness()}
        type="text"
        autoComplete="chrome-off"
        placeholder={name}
        name={name}
        value={value}
        onChange={onChange}
        disabled={disabled}
      ></input>
    </div>
  );
}
