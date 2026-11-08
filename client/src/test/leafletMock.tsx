/**
 * A stand-in for `react-leaflet`, so the map components can be rendered in jsdom and a test can see
 * which layers they draw. Use it as the whole module:
 *
 *     vi.mock("react-leaflet", async () => await import("../../test/leafletMock"));
 *
 * Every component becomes a `<div data-testid="<Name>">` that renders its children, with the props a
 * test needs as data attributes. `useMap` returns `fakeMap`. Nothing here behaves like Leaflet: the
 * point is to check *what* is drawn, not how.
 */
import type { ReactNode } from "react";
import { vi } from "vitest";

/** What `useMap()` returns. Its methods are spies; `vi.clearAllMocks()` resets their calls. */
export const fakeMap = {
  getBoundsZoom: vi.fn(() => 5),
  setMinZoom: vi.fn(),
  setMaxZoom: vi.fn(),
  setView: vi.fn(),
  fitBounds: vi.fn(),
  invalidateSize: vi.fn(),
  getContainer: vi.fn(() => document.createElement("div")),
  dragging: { enable: vi.fn(), disable: vi.fn() },
  keyboard: { enable: vi.fn(), disable: vi.fn() },
  boxZoom: { enable: vi.fn(), disable: vi.fn() },
};

export function useMap() {
  return fakeMap;
}

interface Children {
  children?: ReactNode;
}

/** The first coordinate of a GeoJSON feature, as "x,y", which tells layers with the same shape apart. */
function firstPoint(data: unknown): string {
  let node = (data as { geometry?: { coordinates?: unknown } } | undefined)?.geometry?.coordinates;
  while (Array.isArray(node) && Array.isArray(node[0])) {
    node = node[0];
  }
  return Array.isArray(node) ? node.join(",") : "";
}

export function MapContainer({ children }: Children) {
  return <div data-testid="MapContainer">{children}</div>;
}

export function TileLayer({ url }: { url: string }) {
  return <div data-testid="TileLayer" data-url={url} />;
}

export function GeoJSON({
  data,
  pathOptions,
  children,
}: Children & {
  data: { properties?: { name?: string } };
  pathOptions?: { color?: string; dashArray?: string; fill?: boolean };
}) {
  return (
    <div
      data-testid="GeoJSON"
      data-name={data?.properties?.name ?? ""}
      data-first-point={firstPoint(data)}
      data-color={pathOptions?.color ?? ""}
      data-dash={pathOptions?.dashArray ?? ""}
    >
      {children}
    </div>
  );
}

export function Marker({ position, children }: Children & { position: [number, number] }) {
  return (
    <div data-testid="Marker" data-position={JSON.stringify(position)}>
      {children}
    </div>
  );
}

export function Tooltip({ children }: Children) {
  return <div data-testid="Tooltip">{children}</div>;
}

export function Polyline({ positions }: { positions: unknown }) {
  return <div data-testid="Polyline" data-positions={JSON.stringify(positions)} />;
}

export function ScaleControl({ position }: { position?: string }) {
  return <div data-testid="ScaleControl" data-position={position ?? ""} />;
}

/** One drawn GeoJSON layer. `point` is its first coordinate, as "x,y". */
export interface DrawnLayer {
  name: string;
  point: string;
  color: string;
  dashed: boolean;
}

/** The GeoJSON layers currently rendered, in drawing order. */
export function drawnLayers(): DrawnLayer[] {
  return Array.from(document.querySelectorAll<HTMLElement>('[data-testid="GeoJSON"]')).map((el) => ({
    name: el.dataset.name ?? "",
    point: el.dataset.firstPoint ?? "",
    color: el.dataset.color ?? "",
    dashed: (el.dataset.dash ?? "") !== "",
  }));
}

/** The `url` of each tile layer currently rendered. */
export function drawnTileUrls(): string[] {
  return Array.from(document.querySelectorAll<HTMLElement>('[data-testid="TileLayer"]')).map((el) => el.dataset.url ?? "");
}

/** The position of each marker currently rendered, as [lat, lng]. */
export function drawnMarkers(): [number, number][] {
  return Array.from(document.querySelectorAll<HTMLElement>('[data-testid="Marker"]')).map((el) => JSON.parse(el.dataset.position ?? "[]"));
}
