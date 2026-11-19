.PHONY: test test-backend test-frontend

# Full build — as CI does: compile, all tests, coverage check
test:
	mvn clean install

# Backend tests only — skips the Playwright end-to-end tests (faster for backend-focused iteration)
test-backend:
	mvn test -Dtest='!**/e2e/**'

# Frontend tests only — the Playwright end-to-end tests
test-frontend:
	mvn test -Dtest='**/e2e/**'
