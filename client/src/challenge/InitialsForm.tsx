import { useId, useState } from "react";
import { useSetInitials } from "../api/challenges";
import { hasStatus } from "../api/http";
import { Button, Notice, Spinner } from "../ui";

const MAX_LENGTH = 3;

/** Arcade-style initials for a score near the top of a league table. `initials` are the ones already saved. */
export default function InitialsForm({ gameId, initials }: { gameId: string; initials: string }): React.JSX.Element {
  const inputId = useId();
  const [value, setValue] = useState(initials);
  const save = useSetInitials(gameId);

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    save.reset();
    const letters = e.currentTarget.value.replace(/[^a-z]/gi, "");
    setValue(letters.slice(0, MAX_LENGTH).toUpperCase());
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (value && !save.isPending) save.mutate(value);
  }

  return (
    <form className="gg-initials" onSubmit={handleSubmit}>
      <label className="gg-initials__label gg-mono" htmlFor={inputId}>
        Your initials
      </label>
      <div className="gg-initials__row">
        <input
          id={inputId}
          className="gg-input gg-mono"
          type="text"
          name="initials"
          maxLength={MAX_LENGTH}
          autoCapitalize="characters"
          autoComplete="off"
          spellCheck={false}
          value={value}
          onChange={handleChange}
        />
        <Button type="submit" variant="secondary" disabled={!value || save.isPending}>
          {save.isPending ? <Spinner size="sm" /> : "Save"}
        </Button>
      </div>
      {save.isError && (
        <Notice>
          {hasStatus(save.error, 400)
            ? "Those initials can't be used. Try others."
            : "Couldn't save your initials. Try again."}
        </Notice>
      )}
    </form>
  );
}
