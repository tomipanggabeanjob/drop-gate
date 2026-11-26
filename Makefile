.PHONY: up down test logs loadtest

up:
	docker compose --profile core --profile search up --build -d

down:
	docker compose --profile core --profile search down -v

test:
	mvn test
	cd dashboard && npm install && npm run build

logs:
	docker compose --profile core --profile search logs -f

loadtest:
	@echo "Load tests are defined in Phase 5. Nothing to run yet."
