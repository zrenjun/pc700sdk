//#include "Outputdata.h"
#include "sigleEcg.h"

/******************************************************************************************************
**  Name:       median
**  Function:   Fast check median value algorithm.
**  Input:      org_data
**  Output:     median value for the buffer.
*******************************************************************************************************/
#define MEDFILT2_LEN        (135)//135

int GetMedian(int data, int init) {
    static int array1[MEDFILT2_LEN], dlen;
    int temp, arr[MEDFILT2_LEN];
    int low, high;
    int median_index;
    int middle, ll, hh;

    if (init) {
        for (temp = 0; temp < MEDFILT2_LEN; temp++)
            array1[temp] = 0;
        dlen = 0;
        return 0;
    }

    for (temp = MEDFILT2_LEN-1; temp > 0; temp--) {
        array1[temp] = array1[temp - 1];
    }
    array1[0] = data;

    if (dlen < MEDFILT2_LEN) dlen++;

    for (temp = 0; temp < dlen; temp++)
        arr[temp] = array1[temp];

    low = 0;
    high = dlen - 1;
    median_index = (low + high) >> 1;

    for (;;) {
        if (high <= low) /* One element only */
            return arr[median_index];

        if (high == low + 1) {  /* Two elements only */
            if (arr[low] > arr[high]) {
                temp = arr[low];
                arr[low] = arr[high];
                arr[high] = temp;
            }
            return arr[median_index];
        }

        /* Find median of low, middle and high items; swap into position low */
        middle = (low + high) >> 1;
        if (arr[middle] > arr[high]) {
            temp = arr[middle];
            arr[middle] = arr[high];
            arr[high] = temp;
        }
        if (arr[low] > arr[high]) {
            temp = arr[low];
            arr[low] = arr[high];
            arr[high] = temp;
        }
        if (arr[middle] > arr[low]) {
            temp = arr[middle];
            arr[middle] = arr[low];
            arr[low] = temp;
        }

        /* Swap low item (now in position middle) into position (low+1) */
        temp = arr[middle];
        arr[middle] = arr[low + 1];
        arr[low + 1] = temp;

        /* Nibble from each end towards middle, swapping items when stuck */
        ll = low + 1;
        hh = high;
        for (;;) {
            //do ll++; while (arr[low] > arr[ll]) ;
            //do hh--; while (arr[hh]  > arr[low]) ;
            while (ll < dlen - 1) {
                ll++;
                if (arr[low] > arr[ll]) continue;
                else break;
            }
            while (hh > 0) {
                hh--;
                if (arr[hh] > arr[low]) continue;
                else break;
            }

            if (hh < ll) break;
            temp = arr[ll];
            arr[ll] = arr[hh];
            arr[hh] = temp;
        }

        /* Swap middle item (in position low) back into correct position */
        temp = arr[low];
        arr[low] = arr[hh];
        arr[hh] = temp;

        /* Re-set active partition */
        if (hh <= median_index)
            low = ll;
        if (hh >= median_index)
            high = hh - 1;
    }
}


/*******************************************************************************
** Function:    GetMean
** Description: Returns the mean of an array1 of integers .

** Input:    data
** Output:   GetMean
*******************************************************************************/
typedef long long int64;
#define MEANNUM  (224)    // N = 0.443*fs/foc; //224 =0.3 //133 =0.5

int GetMean(int data, int init) {
    static int64 sum;
    int i;
    static int databuff[MEANNUM];
    static int databuff_cnt, databuff_flag;

    if (init) {
        for (i = 0; i < MEANNUM; ++i) {
            databuff[i] = 0;
        }
        sum = 0;
        databuff_cnt = 0;
        databuff_flag = 0;
        return 0;
    }

    if (databuff_flag == 0) {
        sum = sum + data;
        databuff[databuff_cnt] = data;
        databuff_cnt++;

        if (databuff_cnt == MEANNUM) {
            databuff_cnt = 0;
            databuff_flag = 1;
            return (int) (sum / MEANNUM);
        } else {
            return (int) (sum / databuff_cnt);
        }
    } else {
        sum = sum + data - databuff[databuff_cnt];
        databuff[databuff_cnt] = data;
        databuff_cnt++;
        if (databuff_cnt == MEANNUM) databuff_cnt = 0;

        return (int) (sum / MEANNUM);
    }
}

int HPFilter_05Hz(int data, int init) {
    int res, temp;
    if (init) {
        GetMean(0, 1);
        GetMedian(0, 1);
        return 0;
    }
    temp = GetMean(data, 0);
    res = data - GetMedian(temp, 0);//GetMedian(temp, 0);

    return res;
}
