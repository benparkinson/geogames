/**
 * Describes the shape of a JSON value as another JSON value, for the contract with the backend. The
 * backend has the same rules in `src/test/java/.../JsonShape.java`; change them together.
 *
 * - An object becomes an object with the same keys, each mapped to its child's shape.
 * - An array becomes `{ array: <merged shape of its elements> }`, and an empty array
 *   `{ array: "empty" }`.
 * - Primitives become "string", "number" or "boolean"; null becomes "null".
 * - The value of any key named `coordinates` becomes "coordinates", because GeoJSON nesting depth
 *   varies between geometry types.
 * - Merging two object shapes takes the union of their keys. "empty" and "null" give way to the
 *   other shape. Two different primitives join with "|", sorted ("number|string").
 */
export type Shape = string | { [key: string]: Shape };

const EMPTY = "empty";
const NULL = "null";

export function shapeOf(value: unknown): Shape {
  if (Array.isArray(value)) {
    return { array: value.reduce<Shape>((merged, element) => mergeShapes(merged, shapeOf(element)), EMPTY) };
  }
  if (value === null || value === undefined) {
    return NULL;
  }
  if (typeof value === "object") {
    const shape: { [key: string]: Shape } = {};
    for (const key of Object.keys(value).sort()) {
      shape[key] = key === "coordinates" ? "coordinates" : shapeOf((value as Record<string, unknown>)[key]);
    }
    return shape;
  }
  if (typeof value === "boolean") {
    return "boolean";
  }
  return typeof value === "number" ? "number" : "string";
}

export function mergeShapes(a: Shape, b: Shape): Shape {
  if (JSON.stringify(a) === JSON.stringify(b) || b === EMPTY || b === NULL) {
    return a;
  }
  if (a === EMPTY || a === NULL) {
    return b;
  }
  if (typeof a === "object" && typeof b === "object") {
    const union: { [key: string]: Shape } = { ...a };
    for (const [key, value] of Object.entries(b)) {
      union[key] = key in union ? mergeShapes(union[key], value) : value;
    }
    return union;
  }
  if (typeof a === "string" && typeof b === "string") {
    return [...new Set([...a.split("|"), ...b.split("|")])].sort().join("|");
  }
  throw new Error(`Cannot merge the shapes ${JSON.stringify(a)} and ${JSON.stringify(b)}`);
}
