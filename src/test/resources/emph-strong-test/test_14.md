Blaa

**SELECT @default_late_fees =
     CASE @default_days_out % @default_product_price_days_out
               <br>WHEN 0 <br>THEN (@default_days_out / @default_product_price_days_out) * @default_amount 
               <br>ELSE (FLOOR(@default_days_out / @default_product_price_days_out) + 1) * @default_amount 
          <br> END**

Thanks,
<br>S