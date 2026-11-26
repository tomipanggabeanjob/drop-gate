import { Link, NavLink, Route, Routes, useParams } from 'react-router-dom';
import { BrowserRouter } from 'react-router-dom';
import { useEffect, useMemo, useState } from 'react';

const demoDrops = [
  { id: 'drop-42', name: 'Nintendo Switch OLED', totalStock: 120, price: '$349', liveQueue: 31 },
  { id: 'drop-77', name: 'AirPods Max', totalStock: 48, price: '$549', liveQueue: 18 },
  { id: 'drop-88', name: 'Sony WH-1000XM5', totalStock: 90, price: '$399', liveQueue: 44 },
];

function App() {
  return (
    <BrowserRouter>
      <div className="app-root">
        <header className="topbar">
          <div>
            <p className="eyebrow">DropGate</p>
            <h1>Flash sale control room</h1>
          </div>
          <nav className="nav">
            <NavLink to="/">Shopper</NavLink>
            <NavLink to="/operator">Operator</NavLink>
          </nav>
        </header>

        <Routes>
          <Route path="/" element={<DropListPage />} />
          <Route path="/drops/:id" element={<DropShopperPage />} />
          <Route path="/operator" element={<OperatorPage />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}

function DropListPage() {
  return (
    <main className="page-grid">
      {demoDrops.map((drop) => (
        <Link key={drop.id} to={`/drops/${drop.id}`} className="drop-card">
          <div className="drop-card-header">
            <span className="chip">Live</span>
            <span>{drop.liveQueue} in queue</span>
          </div>
          <h2>{drop.name}</h2>
          <p>{drop.price}</p>
          <div className="stock-row">
            <div className="stock-bar">
              <span style={{ width: `${Math.max(20, (drop.totalStock / 120) * 100)}%` }} />
            </div>
            <strong>{drop.totalStock} left</strong>
          </div>
        </Link>
      ))}
    </main>
  );
}

function DropShopperPage() {
  const { id } = useParams();
  const [position, setPosition] = useState<number>(16);
  const [status, setStatus] = useState<'queued' | 'admitted' | 'holding' | 'purchased' | 'throttled'>('queued');
  const [countdown, setCountdown] = useState(28);
  const [fingerprint, setFingerprint] = useState('');

  useEffect(() => {
    const browserSignature = `${navigator.userAgent}-${new Date().toISOString().slice(0, 10)}`;
    setFingerprint(createFingerprint(browserSignature));
  }, []);

  useEffect(() => {
    const source = new EventSource(`https://example.invalid/queue/stream?dropId=${id}`);
    const interval = window.setInterval(() => {
      setPosition((current) => Math.max(1, current - 1));
    }, 2000);

    source.onerror = () => {
      setStatus((current) => (current === 'queued' ? 'queued' : current));
    };

    return () => {
      source.close();
      window.clearInterval(interval);
    };
  }, [id]);

  useEffect(() => {
    if (status !== 'admitted' && status !== 'holding') {
      return;
    }

    const timer = window.setInterval(() => {
      setCountdown((current) => {
        if (current <= 1) {
          setStatus('holding');
          return 0;
        }
        return current - 1;
      });
    }, 1000);

    return () => window.clearInterval(timer);
  }, [status]);

  const drop = useMemo(
    () => demoDrops.find((entry) => entry.id === id) ?? demoDrops[0],
    [id]
  );

  const joinQueue = () => {
    setStatus('queued');
    setPosition(12);
  };

  const confirmCheckout = () => {
    setStatus('purchased');
    setCountdown(0);
  };

  return (
    <main className="detail-layout">
      <section className="panel">
        <p className="eyebrow">Drop</p>
        <h2>{drop.name}</h2>
        <div className="metrics">
          <div>
            <span>Price</span>
            <strong>{drop.price}</strong>
          </div>
          <div>
            <span>Available</span>
            <strong>{drop.totalStock}</strong>
          </div>
          <div>
            <span>Queue</span>
            <strong>{drop.liveQueue}</strong>
          </div>
        </div>

        <div className="queue-box">
          <span className="status-pill status-${status}">{status.toUpperCase()}</span>
          <h3>Queue position</h3>
          <div className="position-value">#{position}</div>
          <p>Fingerprint: {fingerprint || 'pending'}</p>
        </div>

        <div className="cta-row">
          <button type="button" onClick={joinQueue}>Join queue</button>
          <button type="button" className="secondary" onClick={() => setStatus('admitted')}>
            Simulate admission
          </button>
          <button type="button" className="secondary" onClick={confirmCheckout}>
            Checkout
          </button>
        </div>

        <div className="countdown-box">
          <span>Hold timer</span>
          <strong>{countdown}s</strong>
        </div>
      </section>
    </main>
  );
}

function OperatorPage() {
  return (
    <main className="panel operator-layout">
      <div className="operator-header">
        <div>
          <p className="eyebrow">Operator</p>
          <h2>Live drop telemetry</h2>
        </div>
        <span className="chip good">Healthy</span>
      </div>

      <div className="metrics">
        <div>
          <span>Stock</span>
          <strong>91 / 120</strong>
        </div>
        <div>
          <span>Queue</span>
          <strong>27</strong>
        </div>
        <div>
          <span>Bot risk</span>
          <strong>2.3%</strong>
        </div>
      </div>

      <div className="feed-table">
        <table>
          <thead>
            <tr>
              <th>Fingerprint</th>
              <th>Risk</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>fp-1a4</td>
              <td>81</td>
              <td>Throttle</td>
            </tr>
            <tr>
              <td>fp-9cc</td>
              <td>44</td>
              <td>Monitor</td>
            </tr>
            <tr>
              <td>fp-2dd</td>
              <td>11</td>
              <td>Allow</td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>
  );
}

function createFingerprint(seed: string) {
  return Array.from(seed)
    .reduce((acc, char) => ((acc * 31 + char.charCodeAt(0)) >>> 0) % 1000000, 0)
    .toString(16)
    .padStart(6, '0');
}

export default App;
