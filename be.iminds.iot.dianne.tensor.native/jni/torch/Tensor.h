/*******************************************************************************
 * DIANNE  - Framework for distributed artificial neural networks
 * Copyright (C) 2015  iMinds - IBCN - UGent
 *
 * This file is part of DIANNE.
 *
 * DIANNE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Tim Verbelen, Steven Bohez
 *******************************************************************************/
#ifndef TENSOR_H
#define TENSOR_H

#include "torch7/lib/TH/TH.h"

#ifdef THTensor_
#undef THTensor_
#endif
#ifdef THStorage_
#undef THStorage_
#endif
#ifdef real
#undef real
#endif
#ifdef accreal
#undef accreal
#endif
#ifdef THTensor
#undef THTensor
#endif
#ifdef THStorage
#undef THStorage
#endif
typedef float real;
typedef double accreal;
typedef THFloatTensor THTensor;
typedef THFloatStorage THStorage;
#define THTensor_(x) THFloatTensor_##x
#define THStorage_(x) THFloatStorage_##x
#define Real Float

#endif
