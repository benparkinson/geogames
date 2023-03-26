import { useQuery, useQueryClient } from "react-query";
import { MapContainer, TileLayer, Marker } from "react-leaflet";
import { GuessBox } from "./GuessBox";
import { useState } from "react";
import { Button } from "./Button";
import { CenterUpdater } from "../map/CenterUpdater";
import { SERVER_ENDPOINT } from "../api/Gateway";

const TRIPOINT_LOOKUP = "tripoints";

export function Tripoint() {
  const { isLoading, error, data } = useQuery(TRIPOINT_LOOKUP, () =>
    fetch(SERVER_ENDPOINT + "/api/tripoint").then((result) => result.json())
  );
  const [guesses, setGuesses] = useState(Array(5).fill(""));
  const [correctnessArray, setCorrectnessArray] = useState(Array(5).fill(null));
  const [gaveUp, setGaveUp] = useState(false);
  const queryClient = useQueryClient();

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
        correct={correctnessArray[index]}
        disabled={gaveUp}
      />
    ));
  }

  function handleGuessInput({ target }) {
    const newGuesses = guesses.map((guess, i) => {
      if (guessBoxName(i) === target.name && !correctnessArray[i]) {
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
        <CenterUpdater center={[tripoint.coordinate.latitude, tripoint.coordinate.longitude]} />
      </MapContainer>
    );
  }

  function submitGuesses() {
    const correctAnswers = new Set(data.countryNames.slice());
    const newCorrectness = correctnessArray.slice();
    guesses.forEach((guess, index) => {
      if (correctAnswers.delete(guess)) {
        newCorrectness[index] = true;
      } else {
        newCorrectness[index] = false;
      }
    });
    setCorrectnessArray(newCorrectness);
  }

  function giveUp() {
    const correctAnswers = data.countryNames.slice();
    const newGuesses = guesses.slice();
    correctAnswers.forEach((country, index) => {
      newGuesses[index] = country;
    });
    setGuesses(newGuesses);
    setGaveUp(true);
  }

  function newTripoint() {
    queryClient.invalidateQueries(TRIPOINT_LOOKUP);
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
            <div className="m-1">
              <Button bootstrapClass="btn-success" text={"Submit"} onClick={submitGuesses} />
            </div>
            <div className="m-1">
              <Button bootstrapClass="btn-secondary" text={"Give Up"} onClick={giveUp} />
            </div>
          </div>

          <div id="result"></div>

          <div id="new-game-div">
            <Button bootstrapClass="btn-primary" text={"New Tripoint"} onClick={newTripoint} />
          </div>
        </div>
      </div>
    </div>
  );
}
