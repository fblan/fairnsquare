.PHONY: test test-backend test-frontend test-parallel

# Full build — sequential, as CI does (backend + frontend)
test:
	mvn clean install

# Backend tests only — skips frontend vitest (faster for backend-focused iteration)
test-backend:
	mvn test -Dquarkus.quinoa.run-tests=false

# Frontend tests only
test-frontend:
	cd fairnsquare-app/src/main/webui && npm run test:run

# Run backend and frontend tests in parallel
# Both must pass; exits with non-zero if either fails
test-parallel:
	@(cd fairnsquare-app/src/main/webui && npm run test:run) & npm_pid=$$! ; \
	mvn test -Dquarkus.quinoa.run-tests=false ; mvn_status=$$? ; \
	wait $$npm_pid ; npm_status=$$? ; \
	exit $$((mvn_status | npm_status))
