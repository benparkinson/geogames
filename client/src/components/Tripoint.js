import { useQuery } from "react-query";
import { MapContainer, TileLayer, Marker } from "react-leaflet";
import { GuessBox } from "./GuessBox";
import { useState } from "react";

export function Tripoint() {
  const { isLoading, error, data } = useQuery("api?", () =>
    fetch("http://localhost:8080/api/tripoint").then((result) => result.json())
  );
  const [guesses, setGuesses] = useState(Array(5).fill(""));

  console.log("got: " + JSON.stringify(data));

  function guessBoxName(index) {
    return "Country " + (index + 1);
  }

  function renderBoxes(tripoint) {
    return tripoint.countryNames.map((name, index) => (
      <GuessBox
        key={index}
        value={guesses[index]}
        name={guessBoxName(index)}
        onChange={handleGuessInput}
      />
    ));
  }

  function handleGuessInput({ target }) {
    const newGuesses = guesses.map((guess, i) => {
      if (guessBoxName(i) === target.name) {
        return target.value;
      }
      return guess;
    });
    setGuesses(newGuesses);
  }

  function renderMap(tripoint) {
    return (
      <MapContainer
        center={[tripoint.coordinate.latitude, tripoint.coordinate.longitude]}
        zoom={4}
        maxZoom={6}
        minZoom={4}
      >
        <TileLayer url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}" />
        <Marker position={[tripoint.coordinate.latitude, tripoint.coordinate.longitude]} />
      </MapContainer>
    );
  }

  return (
    <div className="d-flex flex-column vh-100">
      <div className="container flex-fill">
        <div id="map">{isLoading || renderMap(data)}</div>
        <div id="input" className="row align-items-center">
          <div id="answer-header" className="m-1">
            <h4>Answers:</h4>
          </div>
          <div id="text-boxes" className="column justify-content-center align-items-center">
            {isLoading || renderBoxes(data)}
          </div>

          <div className="m-1 column justify-content-center align-items-center">
            <div>
              <button type="button" id="guess" className="m-1 btn btn-success">
                Submit
              </button>
            </div>
            <div>
              <button type="button" id="give-up" className="m-1 btn btn-secondary">
                Give Up
              </button>
            </div>
          </div>

          <div id="result"></div>

          <div id="new-game-div">
            <button type="button" id="new-game" className="btn btn-primary">
              New Tripoint
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
