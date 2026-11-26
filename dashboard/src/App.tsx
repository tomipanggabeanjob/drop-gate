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
  const [stockByDrop, setStockByDrop] = useState<Record<string, number>>({});

  useEffect(() => {
    let cancelled = false;
    Promise.all(
      demoDrops.map(async (drop) => {
        try {
          const response = await fetch(`/api/inventory/${drop.id}`);
          if (!response.ok) {
            return [drop.id, drop.totalStock] as const;
          }
          const body = await response.json();
          return [drop.id, Number(body.available ?? drop.totalStock)] as const;
        } catch {
          return [drop.id, drop.totalStock] as const;
        }
      })
    ).then((entries) => {
      if (!cancelled) {
        setStockByDrop(Object.fromEntries(entries));
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <main className="page-grid">
      {demoDrops.map((drop) => {
        const available = stockByDrop[drop.id] ?? drop.totalStock;
        return (
          <Link key={drop.id} to={`/drops/${drop.id}`} className="drop-card">
            <div className="drop-card-header">
              <span className="chip">Live</span>
              <span>{drop.liveQueue} in queue</span>
            </div>
            <h2>{drop.name}</h2>
            <p>{drop.price}</p>
            <div className="stock-row">
              <div className="stock-bar">
                <span style={{ width: `${Math.max(8, (available / Math.max(drop.totalStock, 1)) * 100)}%` }} />
              </div>
              <strong>{available} left</strong>
            </div>
          </Link>
        );
      })}
    </main>
  );
}

function DropShopperPage() {
  const { id } = useParams();
  const dropId = id ?? 'drop-42';
  const accountId = useMemo(() => `acct-${createFingerprint(dropId + '-shopper')}`, [dropId]);
  const [position, setPosition] = useState<number | null>(null);
  const [status, setStatus] = useState<'idle' | 'queued' | 'admitted' | 'holding' | 'purchased' | 'throttled' | 'error'>('idle');
  const [countdown, setCountdown] = useState(30);
  const [fingerprint, setFingerprint] = useState('');
  const [available, setAvailable] = useState<number | null>(null);
  const [holdId, setHoldId] = useState<string | null>(null);
  const [message, setMessage] = useState('');
  const [token, setToken] = useState<string | null>(null);

  const drop = useMemo(
    () => demoDrops.find((entry) => entry.id === dropId) ?? demoDrops[0],
    [dropId]
  );

  useEffect(() => {
    const browserSignature = `${navigator.userAgent}-${new Date().toISOString().slice(0, 10)}`;
    setFingerprint(createFingerprint(browserSignature));
  }, []);

  useEffect(() => {
    let cancelled = false;
    const loadInventory = async () => {
      try {
        const response = await fetch(`/api/inventory/${dropId}`);
        if (!response.ok) {
          return;
        }
        const body = await response.json();
        if (!cancelled) {
          setAvailable(Number(body.available));
        }
      } catch {
        // keep demo stock if inventory is unavailable
      }
    };
    loadInventory();
    const timer = window.setInterval(loadInventory, 3000);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [dropId]);

  useEffect(() => {
    if (!fingerprint || status === 'idle' || status === 'purchased' || status === 'throttled') {
      return;
    }

    const source = new EventSource(`/api/drops/${dropId}/queue/stream?accountId=${encodeURIComponent(accountId)}`);
    source.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data);
        if (typeof payload.queuePosition === 'number') {
          setPosition(payload.queuePosition);
          if (payload.queuePosition <= 1) {
            setStatus((current) => (current === 'queued' ? 'admitted' : current));
          }
        }
      } catch {
        // ignore malformed SSE payloads
      }
    };
    source.onerror = () => {
      source.close();
    };

    const poll = window.setInterval(async () => {
      try {
        const response = await fetch(
          `/api/drops/${dropId}/queue/position?accountId=${encodeURIComponent(accountId)}`
        );
        if (!response.ok) {
          return;
        }
        const body = await response.json();
        if (typeof body.queuePosition === 'number') {
          setPosition(body.queuePosition);
          if (body.queuePosition <= 1) {
            setStatus((current) => (current === 'queued' ? 'admitted' : current));
          }
        }
      } catch {
        // keep current UI state
      }
    }, 2000);

    return () => {
      source.close();
      window.clearInterval(poll);
    };
  }, [accountId, dropId, fingerprint, status]);

  useEffect(() => {
    if (status !== 'holding') {
      return;
    }

    const timer = window.setInterval(() => {
      setCountdown((current) => {
        if (current <= 1) {
          setMessage('Hold expired — join again to retry');
          setStatus('idle');
          setHoldId(null);
          return 0;
        }
        return current - 1;
      });
    }, 1000);

    return () => window.clearInterval(timer);
  }, [status]);

  const joinQueue = async () => {
    setMessage('');
    try {
      const response = await fetch(`/api/drops/${dropId}/queue/join`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ dropId, accountId, fingerprint }),
      });
      if (response.status === 429) {
        setStatus('throttled');
        setMessage('Rate limited — try again shortly');
        return;
      }
      if (!response.ok) {
        setStatus('error');
        setMessage(`Join failed (${response.status})`);
        return;
      }
      const body = await response.json();
      setToken(body.token ?? null);
      setPosition(Number(body.queuePosition ?? 1));
      setStatus(Number(body.queuePosition ?? 1) <= 1 ? 'admitted' : 'queued');
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : 'Join failed');
    }
  };

  const createHold = async () => {
    setMessage('');
    try {
      const response = await fetch(`/api/drops/${dropId}/holds`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ accountId, quantity: 1 }),
      });
      if (!response.ok) {
        setStatus('error');
        setMessage(`Hold failed (${response.status})`);
        return;
      }
      const body = await response.json();
      setHoldId(body.holdId);
      setAvailable(Number(body.remaining));
      setCountdown(30);
      setStatus('holding');
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : 'Hold failed');
    }
  };

  const confirmCheckout = async () => {
    if (!holdId) {
      setMessage('Create a hold before checkout');
      return;
    }
    setMessage('');
    try {
      const response = await fetch('/api/checkout', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': `checkout-${accountId}-${holdId}`,
        },
        body: JSON.stringify({
          dropId,
          accountId,
          holdId,
          quantity: 1,
          totalCents: Number(String(drop.price).replace(/[^0-9]/g, '')) * 100 || 2500,
        }),
      });
      if (!response.ok) {
        setStatus('error');
        setMessage(`Checkout failed (${response.status})`);
        return;
      }
      setStatus('purchased');
      setCountdown(0);
      setMessage('Payment confirmed');
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : 'Checkout failed');
    }
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
            <strong>{available ?? drop.totalStock}</strong>
          </div>
          <div>
            <span>Queue</span>
            <strong>{position ?? '—'}</strong>
          </div>
        </div>

        <div className="queue-box">
          <span className={`status-pill status-${status}`}>{status.toUpperCase()}</span>
          <h3>Queue position</h3>
          <div className="position-value">#{position ?? '—'}</div>
          <p>Fingerprint: {fingerprint || 'pending'}</p>
          <p>Account: {accountId}</p>
          {token ? <p>Token: {token.slice(0, 8)}…</p> : null}
          {message ? <p>{message}</p> : null}
        </div>

        <div className="cta-row">
          <button type="button" onClick={joinQueue}>Join queue</button>
          <button
            type="button"
            className="secondary"
            onClick={createHold}
            disabled={status !== 'admitted' && status !== 'queued'}
          >
            Create hold
          </button>
          <button
            type="button"
            className="secondary"
            onClick={confirmCheckout}
            disabled={status !== 'holding' || !holdId}
          >
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
  const [stock, setStock] = useState<number | null>(null);
  const [queuePosition, setQueuePosition] = useState<number | null>(null);
  const [health, setHealth] = useState('checking');

  useEffect(() => {
    let cancelled = false;
    const refresh = async () => {
      try {
        const [inventoryResponse, healthResponse] = await Promise.all([
          fetch('/api/inventory/drop-42'),
          fetch('/'),
        ]);
        if (!cancelled) {
          if (inventoryResponse.ok) {
            const inventory = await inventoryResponse.json();
            setStock(Number(inventory.available));
          }
          setHealth(healthResponse.ok ? 'Healthy' : 'Degraded');
        }
      } catch {
        if (!cancelled) {
          setHealth('Degraded');
        }
      }

      try {
        const response = await fetch('/api/drops/drop-42/queue/position?accountId=acct-operator');
        if (response.ok && !cancelled) {
          const body = await response.json();
          setQueuePosition(Number(body.queuePosition ?? 0));
        }
      } catch {
        // queue may be empty for the operator probe account
      }
    };

    refresh();
    const timer = window.setInterval(refresh, 3000);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, []);

  return (
    <main className="panel operator-layout">
      <div className="operator-header">
        <div>
          <p className="eyebrow">Operator</p>
          <h2>Live drop telemetry</h2>
        </div>
        <span className={`chip ${health === 'Healthy' ? 'good' : ''}`}>{health}</span>
      </div>

      <div className="metrics">
        <div>
          <span>Stock</span>
          <strong>{stock ?? '—'}</strong>
        </div>
        <div>
          <span>Queue probe</span>
          <strong>{queuePosition ?? '—'}</strong>
        </div>
        <div>
          <span>Bot risk</span>
          <strong>live</strong>
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
