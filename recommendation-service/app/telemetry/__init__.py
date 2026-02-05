import logging
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.resources import Resource, SERVICE_NAME
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.instrumentation.sqlalchemy import SQLAlchemyInstrumentor
from opentelemetry.instrumentation.httpx import HTTPXClientInstrumentor
from opentelemetry.instrumentation.logging import LoggingInstrumentor
from app.settings import get_settings

settings = get_settings()
logger = logging.getLogger(__name__)


def setup_telemetry():
    """
    Initialize OpenTelemetry instrumentation for distributed tracing.
    Sends traces to Jaeger via OTLP (OpenTelemetry Protocol).
    """
    
    # Define service resource
    resource = Resource(attributes={
        SERVICE_NAME: settings.SERVICE_NAME
    })
    
    # Create TracerProvider
    trace_provider = TracerProvider(resource=resource)
    
    # Configure OTLP Exporter (HTTP endpoint for Jaeger)
    otlp_exporter = OTLPSpanExporter(
        endpoint=f"{settings.OTEL_EXPORTER_OTLP_ENDPOINT}/v1/traces",
        timeout=10
    )
    
    # Add BatchSpanProcessor (better performance than SimpleSpanProcessor)
    trace_provider.add_span_processor(BatchSpanProcessor(otlp_exporter))
    
    # Set global tracer provider
    trace.set_tracer_provider(trace_provider)
    
    # Auto-instrument libraries
    LoggingInstrumentor().instrument(set_logging_format=True)
    HTTPXClientInstrumentor().instrument()
    
    logger.info(f"✅ OpenTelemetry initialized. Sending traces to {settings.OTEL_EXPORTER_OTLP_ENDPOINT}")


def instrument_fastapi(app):
    """
    Instrument FastAPI app for automatic request/response tracing.
    Must be called AFTER app creation.
    """
    FastAPIInstrumentor.instrument_app(app)
    logger.info("✅ FastAPI instrumented for tracing")


def instrument_sqlalchemy(engine):
    """
    Instrument SQLAlchemy engine for database query tracing.
    Must be called AFTER engine creation.
    """
    SQLAlchemyInstrumentor().instrument(
        engine=engine.sync_engine,
        service=settings.SERVICE_NAME,
        enable_commenter=True
    )
    logger.info("✅ SQLAlchemy instrumented for tracing")


def get_tracer(name: str):
    """Get a tracer instance for manual span creation"""
    return trace.get_tracer(name)
