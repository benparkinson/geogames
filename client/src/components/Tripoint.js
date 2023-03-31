import { useQuery, useQueryClient } from "react-query";
import { MapContainer, TileLayer, Marker, Polyline, GeoJSON, Tooltip } from "react-leaflet";
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
  const [guesses, setGuesses] = useState(Array(3).fill(""));
  const [correctnessArray, setCorrectnessArray] = useState(Array(3).fill(null));
  const [gaveUp, setGaveUp] = useState(false);
  const [gameOver, setGameOver] = useState(false);
  const queryClient = useQueryClient();

  function canRender() {
    return !error && !isLoading;
  }

  function guessBoxName(index) {
    return "Country " + (index + 1);
  }

  function countryNames(tripoint) {
    return tripoint.countries.map((country) => country.properties.name);
  }

  function renderBoxes(tripoint) {
    return countryNames(tripoint).map((name, index) => (
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
        setCorrectnessByIndex(null, i);
        return target.value;
      }
      return guess;
    });
    setGuesses(newGuesses);
  }

  function setCorrectnessByIndex(correctness, index) {
    const arrayCopy = correctnessArray.slice();
    arrayCopy[index] = correctness;
    setCorrectnessArray(arrayCopy);
  }

  function renderMap(tripoint) {
    const color = gaveUp ? "darkred" : "LightGoldenRodYellow";

    const borders = tripoint.countries.map((country) => (
      <GeoJSON key={country.properties.name} data={country} color={color}>
        <Tooltip>{country.properties.name}</Tooltip>
      </GeoJSON>
    ));

    return (
      <MapContainer
        center={[tripoint.coordinate.latitude, tripoint.coordinate.longitude]}
        zoom={4}
        maxZoom={6}
        minZoom={4}
      >
        <TileLayer url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}" />
        <Marker position={[tripoint.coordinate.latitude, tripoint.coordinate.longitude]} />
        {gameOver && borders}

        <CenterUpdater center={[tripoint.coordinate.latitude, tripoint.coordinate.longitude]} />
      </MapContainer>
    );
  }

  function submitGuesses() {
    const correctAnswers = new Set(countryNames(data).slice());
    const newCorrectness = correctnessArray.slice();
    guesses.forEach((guess, index) => {
      if (correctAnswers.delete(guess)) {
        newCorrectness[index] = true;
      } else {
        newCorrectness[index] = false;
      }
    });
    setCorrectnessArray(newCorrectness);
    setGameOver(newCorrectness.every((answer) => answer));
  }

  function giveUp() {
    const correctAnswers = countryNames(data).slice();
    const newGuesses = guesses.slice();
    correctAnswers.forEach((country, index) => {
      newGuesses[index] = country;
    });
    setGuesses(newGuesses);
    setGaveUp(true);
    setGameOver(true);
  }

  function newTripoint() {
    setGaveUp(false);
    setGameOver(false);
    setCorrectnessArray(Array(3).fill(null));
    setGuesses(Array(3).fill(""));
    queryClient.invalidateQueries(TRIPOINT_LOOKUP);
  }

  return (
    <div className="d-flex flex-column vh-100">
      <div className="container flex-fill">
        <div id="map">{canRender() && renderMap(data)}</div>
        <div id="input" className="row align-items-center">
          <div id="answer-header" className="m-1">
            <h4>Answers:</h4>
          </div>
          <div id="text-boxes" className="column justify-content-center align-items-center">
            {canRender() && renderBoxes(data)}
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
