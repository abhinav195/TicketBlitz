"""
OpenTelemetry instrumentation for FastAPI and SQLAlchemy
"""
from opentelemetry import trace
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.sdk.resources import Resource, SERVICE_NAME
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.instrumentation.sqlalchemy import SQLAlchemyInstrumentor
from fastapi import FastAPI
from sqlalchemy.ext.asyncio import AsyncEngine
from app.settings import get_settings
import logging

logger = logging.getLogger(__name__)
settings = get_settings()


def setup_telemetry() -> None:
    """
    Initialize OpenTelemetry with OTLP exporter
    """
    try:
        # Create resource with service name
        resource = Resource(attributes={
            SERVICE_NAME: settings.OTEL_SERVICE_NAME
        })
        
        # Create tracer provider
        provider = TracerProvider(resource=resource)
        
        # Create OTLP exporter
        otlp_exporter = OTLPSpanExporter(
            endpoint=f"{settings.OTEL_EXPORTER_OTLP_ENDPOINT}/v1/traces"
        )
        
        # Add span processor
        provider.add_span_processor(BatchSpanProcessor(otlp_exporter))
        
        # Set as global tracer provider
        trace.set_tracer_provider(provider)
        
        logger.info(f"✓ OpenTelemetry initialized - sending traces to {settings.OTEL_EXPORTER_OTLP_ENDPOINT}")
        
    except Exception as e:
        logger.warning(f"Failed to initialize OpenTelemetry: {e}")


def instrument_fastapi(app: FastAPI) -> None:
    """
    Instrument FastAPI application
    """
    try:
        FastAPIInstrumentor.instrument_app(app)
        logger.info("✓ FastAPI instrumented for tracing")
    except Exception as e:
        logger.warning(f"Failed to instrument FastAPI: {e}")


def instrument_sqlalchemy(engine: AsyncEngine) -> None:
    """
    Instrument SQLAlchemy engine
    """
    try:
        SQLAlchemyInstrumentor().instrument(
            engine=engine.sync_engine,
            enable_commenter=True,
        )
        logger.info("✓ SQLAlchemy instrumented for tracing")
    except Exception as e:
        logger.warning(f"Failed to instrument SQLAlchemy: {e}")
