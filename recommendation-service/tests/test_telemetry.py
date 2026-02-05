import pytest
from unittest.mock import patch, MagicMock
from app.telemetry import setup_telemetry, instrument_fastapi, instrument_sqlalchemy, get_tracer


class TestTelemetry:
    """Test OpenTelemetry configuration"""
    
    def test_setup_telemetry(self, mock_settings):
        """Test OpenTelemetry initialization"""
        with patch("app.telemetry.settings", mock_settings):
            with patch("app.telemetry.TracerProvider") as mock_provider:
                with patch("app.telemetry.OTLPSpanExporter") as mock_exporter:
                    with patch("app.telemetry.trace.set_tracer_provider") as mock_set_provider:
                        with patch("app.telemetry.LoggingInstrumentor") as mock_logging:
                            with patch("app.telemetry.HTTPXClientInstrumentor") as mock_httpx:
                                
                                setup_telemetry()
                                
                                mock_provider.assert_called_once()
                                mock_exporter.assert_called_once()
                                mock_set_provider.assert_called_once()
                                mock_logging.return_value.instrument.assert_called_once()
                                mock_httpx.return_value.instrument.assert_called_once()
    
    def test_instrument_fastapi(self):
        """Test FastAPI instrumentation"""
        mock_app = MagicMock()
        
        with patch("app.telemetry.FastAPIInstrumentor") as mock_instrumentor:
            instrument_fastapi(mock_app)
            
            mock_instrumentor.instrument_app.assert_called_once_with(mock_app)
    
    def test_instrument_sqlalchemy(self, mock_settings):
        """Test SQLAlchemy instrumentation"""
        mock_engine = MagicMock()
        mock_engine.sync_engine = MagicMock()
        
        with patch("app.telemetry.settings", mock_settings):
            with patch("app.telemetry.SQLAlchemyInstrumentor") as mock_instrumentor:
                instrument_sqlalchemy(mock_engine)
                
                mock_instrumentor.return_value.instrument.assert_called_once()
    
    def test_get_tracer(self):
        """Test tracer retrieval"""
        with patch("app.telemetry.trace.get_tracer") as mock_get_tracer:
            mock_tracer = MagicMock()
            mock_get_tracer.return_value = mock_tracer
            
            tracer = get_tracer("test_tracer")
            
            assert tracer == mock_tracer
            mock_get_tracer.assert_called_once_with("test_tracer")
    
    def test_setup_telemetry_with_endpoint(self, mock_settings):
        """Test telemetry setup with custom endpoint"""
        mock_settings.OTEL_EXPORTER_OTLP_ENDPOINT = "http://custom:4318"
        
        with patch("app.telemetry.settings", mock_settings):
            with patch("app.telemetry.TracerProvider"):
                with patch("app.telemetry.OTLPSpanExporter") as mock_exporter:
                    with patch("app.telemetry.trace.set_tracer_provider"):
                        with patch("app.telemetry.LoggingInstrumentor"):
                            with patch("app.telemetry.HTTPXClientInstrumentor"):
                                
                                setup_telemetry()
                                
                                # Verify custom endpoint was used
                                call_kwargs = mock_exporter.call_args.kwargs
                                assert "http://custom:4318/v1/traces" in call_kwargs.get("endpoint", "")
