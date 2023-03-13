import { QueryClient, QueryClientProvider, useQuery } from 'react-query'
import logo from './logo.svg';
import './App.css';

const queryClient = new QueryClient();

function App() {



  return (
    <QueryClientProvider client={queryClient}>
      <Innards />
    </QueryClientProvider>
  );
}

function Innards() {
  const { isLoading, error, data } = useQuery('api?', () =>
    fetch('http://localhost:8080/api/tripoint').then(result => result.json()))

  console.log('got: ' + JSON.stringify(data));

  return <div className="App">
    <header className="App-header">
      <img src={logo} className="App-logo" alt="logo" />
      <p>
        Edit <code>src/App.js</code> and save to reload.
      </p>
      {isLoading && <p>loading!</p>}
      <a
        className="App-link"
        href="https://reactjs.org"
        target="_blank"
        rel="noopener noreferrer"
      >
        Learn React
      </a>
    </header>
  </div>

}

export default App;
