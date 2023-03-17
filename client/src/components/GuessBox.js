export function GuessBox({ value, name, onChange }) {
  return (
    <div className="m-1">
      <input
        className="answer-attempt form-control"
        type="text"
        autoComplete="chrome-off"
        placeholder={name}
        name={name}
        value={value}
        onChange={onChange}
      ></input>
    </div>
  );
}
