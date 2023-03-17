export function Button({ bootstrapClass, text, onClick }) {
  return (
    <button type="button" className={"btn " + bootstrapClass} onClick={onClick}>
      {text}
    </button>
  );
}
