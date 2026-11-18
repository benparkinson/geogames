import { useEffect, useState } from "react";

/** Turns true once `active` has been true for `ms`, and back to false as soon as it stops: "this is taking a while". */
export function useDelayedFlag(active: boolean, ms: number): boolean {
  const [flag, setFlag] = useState(false);

  useEffect(() => {
    if (!active) {
      return;
    }
    const timer = setTimeout(() => setFlag(true), ms);
    return () => {
      clearTimeout(timer);
      setFlag(false);
    };
  }, [active, ms]);

  return flag;
}
