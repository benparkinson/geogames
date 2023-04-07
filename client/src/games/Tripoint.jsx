import TripointMapWrapper from "../map/TripointMapWrapper";
import MapGame from "./common/MapGame";

function Tripoint() {
  function guessBoxName(index) {
    return "Country " + (index + 1);
  }

  function countryNames(tripoint) {
    return tripoint.countries.map((country) => country.properties.name);
  }

  function extractCountries(tripoint) {
    return tripoint.countries.map((country) => country.properties);
  }

  function correctAnswers(tripoint) {
    return countryNames(tripoint).slice();
  }

  function checkAdditionalAnswers(guess, tripoint) {
    const countries = extractCountries(tripoint);

    let normalisedGuess = guess;

    countries.forEach((country) =>
      country.additionalNames.forEach((additionalName) => {
        if (guess.toLowerCase() === additionalName.toLowerCase()) {
          normalisedGuess = country.name;
        }
      })
    );
    return normalisedGuess;
  }

  return (
    <MapGame
      dataName={"Tripoint"}
      serverRoute={"/api/tripoint"}
      guessBoxCount={3}
      guessBoxName={guessBoxName}
      MapComponent={TripointMapWrapper}
      correctAnswersFunction={correctAnswers}
      checkAdditionalAnswers={checkAdditionalAnswers}
    />
  );
}

export default Tripoint;
