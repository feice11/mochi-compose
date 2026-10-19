# Copyright 2026, mochi-compose contributors
# SPDX-License-Identifier: Apache-2.0
"""Regular, per-glyph dot matrices. Curves guide pixel selection, never free dot placement."""
import math
import cv2
import numpy as np
from source_stroke_guides import build as guide

# The logical matrix is uniform inside each icon. Shape families use different
# resolutions/selection rules; no individual dot is moved to follow a curve.
RECIPES={
 'share_variant':(12,'fill'), 'account_outline':(13,'fill'),
 'pin_outline':(11,'line'), 'lock_outline':(10,'fill'),
 'star':(11,'fill'), 'bell_outline':(11,'fill'), 'delete_outline':(12,'fill'),
 'volume_high':(11,'fill'), 'volume_low':(11,'fill'), 'magnify':(11,'line'),
 'scan_helper':(10,'fill'), 'plus':(9,'line'), 'minus':(9,'line'),
 'airplane':(11,'fill'), 'microphone':(11,'fill'), 'link_variant':(11,'line'),
 'home_outline':(11,'fill'), 'pencil':(12,'fill'), 'content_copy':(11,'line'),
 'flag_outline':(11,'line'), 'bluetooth':(11,'line'), 'laptop':(10,'fill'),
 'cellphone':(10,'fill'), 'monitor':(10,'fill'), 'pulse':(11,'line'),
 'chevron_down_circle_outline':(13,'fill'), 'radiobox_marked':(11,'line'),
 'format_list_bulleted':(12,'fill'), 'compass_outline':(13,'fill'),
 'layers_outline':(11,'line'), 'card_multiple_outline':(12,'fill'),
 'arrow_expand_all':(11,'fill'), 'swap_horizontal':(11,'line'),
 'water':(11,'fill'), 'swap_vertical':(11,'line'), 'creation':(11,'fill'),
 'palette_outline':(11,'line'),
}

def build(name):
 count,mode=RECIPES.get(name,(11,'line'));pitch=24/count;selection_radius=(pitch-.46)/2
 paths,closed,anchors,mask=guide(name,pitch=pitch*.45,radius=.7)
 # Align a real circular hole (magnifier/head) with the lattice, preserving
 # a symmetric digital circle instead of sampling it as a square outline.
 origin=np.array([12.,12.]);pixel=mask.shape[0]/24
 n,labels,stats,centers=cv2.connectedComponentsWithStats((~mask).astype('uint8'),8);round_holes=[]
 for lab,(x,y,w,h,area) in enumerate(stats[1:],1):
  if x==0 or y==0 or x+w==mask.shape[1] or y+h==mask.shape[0] or area<50:continue
  if abs(w/h-1)>.08:continue
  cs,_=cv2.findContours((labels==lab).astype('uint8'),cv2.RETR_EXTERNAL,cv2.CHAIN_APPROX_SIMPLE);c=max(cs,key=cv2.contourArea);per=cv2.arcLength(c,True)
  if per and 4*math.pi*cv2.contourArea(c)/per**2>.82:round_holes.append((area,centers[lab]/pixel))
 if round_holes and name in {'magnify','account_outline','radiobox_marked','compass_outline','chevron_down_circle_outline'}:origin=max(round_holes,key=lambda x:x[0])[1]
 limit=10;bitmap=np.zeros((21,21),np.uint8)
 def cell(p):return np.rint((np.array(p)-origin)/pitch).astype(int)+limit
 if mode=='fill':
  for y in range(21):
   for x in range(21):
    pos=origin+(np.array([x,y])-limit)*pitch;cx,cy=np.rint(pos*pixel).astype(int);r=int(pitch*pixel*.36)
    if cx+r<0 or cy+r<0 or cx-r>=mask.shape[1] or cy-r>=mask.shape[0]:continue
    patch=mask[max(0,cy-r):min(mask.shape[0],cy+r+1),max(0,cx-r):min(mask.shape[1],cx+r+1)]
    if patch.size and patch.mean()>.36:bitmap[y,x]=1
 else:
  for path,loop in zip(paths,closed):
   ps=[cell(p) for p in path]
   for a,b in zip(ps,ps[1:]+(ps[:1] if loop else [])):
    cv2.line(bitmap,tuple(a),tuple(b),1,1)
  # One-pixel strokes on the logical matrix; retain meaningful isolated dots.
  # cv2 draws one-cell lines already; thinning them biases sharp corners.
  for p,r in anchors:
   x,y=cell(p)
   if 0<=x<21 and 0<=y<21:bitmap[y,x]=1
 # Preserve a genuine source mirror axis, rather than accepting raster or
 # skeleton traversal bias. Copy one half; never shift individual dot centers.
 for axis in [0,1]:
  reflected=np.flip(mask,axis=1-axis)
  if np.logical_and(mask,reflected).sum()/max(1,np.logical_or(mask,reflected).sum()) < .97:continue
  offset=2*(12-origin[axis])/pitch
  if abs(offset-round(offset))>.05:continue
  offset=round(offset)
  for y in range(21):
   for x in range(21):
    index=[x-limit,y-limit]
    if index[axis]>offset/2:
     index[axis]=offset-index[axis];sx,sy=np.array(index)+limit
     bitmap[y,x]=bitmap[sy,sx] if 0<=sx<21 and 0<=sy<21 else 0
 # Every icon is still one fixed matrix, including curved and diagonal shapes.
 # Reject off-canvas cells, rather than allowing negative NumPy slices to wrap.
 for y,x in np.argwhere(bitmap):
  center=origin+(np.array([x,y])-limit)*pitch
  if min(center-selection_radius)<0 or max(center+selection_radius)>24:bitmap[y,x]=0
 cells=[(int(x-limit),int(y-limit)) for y,x in np.argwhere(bitmap)]
 # Keep selection and cell positions unchanged; only enlarge rendered circles.
 radius=pitch*.47
 points=[(float(origin[0]+x*pitch),float(origin[1]+y*pitch),radius) for x,y in cells]
 return points,{'gridColumns':count,'pitch':pitch,'radius':radius,'origin':origin.tolist(),'cells':cells,'selection':mode}
