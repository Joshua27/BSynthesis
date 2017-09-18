package de.hhu.stups.bsynthesis.services;


public enum UiZoom {
  ZOOM_IN, ZOOM_OUT;

  public boolean isZoomIn() {
    return this.equals(ZOOM_IN);
  }
}
