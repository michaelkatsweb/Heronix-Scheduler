# Prototypes Directory

⚠️ **WARNING: This directory contains experimental/prototype code for research and development.**

## Purpose

This directory contains proof-of-concept and prototype implementations that:
- Are NOT intended for production use
- May have incomplete error handling
- May lack proper integration with the main application
- Are used for experimenting with new scheduling algorithms and approaches

## Current Prototypes

### AdaptiveScheduler.java

**Status:** PROTOTYPE - DO NOT USE IN PRODUCTION

**Purpose:** Experimental hybrid scheduling engine combining:
- Constraint solving
- Adaptive clustering via NeuroCluster Streamer (NCS)
- AI-powered reasoning via Ollama LLM
- FastAPI microservice integration for Python-based clustering

**Why it's here:**
- Contains a `main()` method for standalone testing
- Requires external FastAPI service (not part of main application)
- Requires Ollama to be installed locally
- Has minimal error handling
- Not integrated with Spring framework
- Uses stub implementations for core scheduling logic

**Production Alternative:**
Use `HybridSchedulingSolver.java` in the `com.eduscheduler.solver` package instead.

## Guidelines

1. **Do NOT import** classes from this package into production code
2. **Do NOT deploy** code from this directory
3. **Use for research only** - experiments, algorithm testing, proof-of-concepts
4. When a prototype is production-ready:
   - Refactor and integrate properly
   - Move to appropriate package
   - Add complete error handling
   - Add unit tests
   - Update documentation

## Questions?

See the main documentation or contact the Heronix Scheduling System team.
