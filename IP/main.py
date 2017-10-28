import cv2
import numpy as np
import os, glob

images = glob.glob('../ss gen/images/**/*.png')
for image in images:
    oppath = image[image.find('\\')+1:]
    opfname = oppath[oppath.find('\\')+1:] #filename for output image
    opfldr = oppath[:oppath.find('\\')] #folder name for output eg. section1
    image = image.replace('\\','/') #making it compatible to open input image
    frame = cv2.imread(image, cv2.IMREAD_UNCHANGED)
    hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)

    #define lower and upper limits for color black in HSV
    lower_limit = np.array([0,0,0])
    upper_limit = np.array([0,0,0])
    #generate mask for filtering all colors but black
    mask = cv2.inRange(hsv, lower_limit, upper_limit)
    #inverse the mask to filter out only black
    cv2.bitwise_not(mask,mask)
    #perform and operation using the mask to finally remove black
    res = cv2.bitwise_and(frame, frame, mask=mask)
    
    opfldr = os.path.join('images',opfldr)
    if not os.path.exists(opfldr):
        os.makedirs(opfldr)
    cv2.imwrite(os.path.join(opfldr, opfname), res)
cv2.destroyAllWindows()
