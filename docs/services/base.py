# -*- coding: utf-8 -*-
from abc import ABC, abstractmethod


class Service(ABC):
    """Abstract base class for services.

    This class defines the interface that all services must implement.
    """

    @abstractmethod
    async def start(self) -> None:
        """
        Starts the service, initializing any necessary resources or
        connections.
        """

    @abstractmethod
    async def stop(self) -> None:
        """Stops the service, releasing any acquired resources."""

    @abstractmethod
    async def health(self) -> bool:
        """
        Checks the health of the service.

        Returns:
            True if the service is healthy, False otherwise.
        """


class ServiceLifecycleManagerMixin:
    """Mixin class that provides async lifecycle manager functionality for
    services.

    This mixin can be used with any class that implements the Service
    interface.
    """

    async def __aenter__(self):
        """Async context manager entry."""
        await self.start()
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit."""
        await self.stop()
        return False


class ServiceWithLifecycleManager(Service, ServiceLifecycleManagerMixin):
    """Base class for services that want async lifecycle manager functionality.

    This class combines the Service interface with the context manager mixin,
    providing a convenient base class for most service implementations.

    Note: This is an abstract base class. Subclasses must implement the
    abstract methods from the Service class.
    """

    @abstractmethod
    async def start(self) -> None:
        """Starts the service, initializing any necessary resources or
        connections."""

    @abstractmethod
    async def stop(self) -> None:
        """Stops the service, releasing any acquired resources."""

    @abstractmethod
    async def health(self) -> bool:
        """
        Checks the health of the service.

        Returns:
            True if the service is healthy, False otherwise.
        """
