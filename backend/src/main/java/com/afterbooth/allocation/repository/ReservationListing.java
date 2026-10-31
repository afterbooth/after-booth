package com.afterbooth.allocation.repository;

import com.afterbooth.allocation.domain.Reservation;

public record ReservationListing(Reservation reservation, String itemLabel) {}
